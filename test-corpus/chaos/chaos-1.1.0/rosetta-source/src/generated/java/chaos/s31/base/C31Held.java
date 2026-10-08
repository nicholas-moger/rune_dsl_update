package chaos.s31.base;

import chaos.s31.base.meta.C31HeldMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Helper - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C31Held", builder=C31Held.C31HeldBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C31Held", model="chaos", builder=C31Held.C31HeldBuilderImpl.class, version="1.0.0")
public interface C31Held extends RosettaModelObject {

	C31HeldMeta metaData = new C31HeldMeta();

	/*********************** Getter Methods  ***********************/
	String getH();

	/*********************** Build Methods  ***********************/
	C31Held build();
	
	C31Held.C31HeldBuilder toBuilder();
	
	static C31Held.C31HeldBuilder builder() {
		return new C31Held.C31HeldBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C31Held> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C31Held> getType() {
		return C31Held.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("h"), String.class, getH(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C31HeldBuilder extends C31Held, RosettaModelObjectBuilder {
		C31Held.C31HeldBuilder setH(String h);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("h"), String.class, getH(), this);
		}
		

		C31Held.C31HeldBuilder prune();
	}

	/*********************** Immutable Implementation of C31Held  ***********************/
	class C31HeldImpl implements C31Held {
		private final String h;
		
		protected C31HeldImpl(C31Held.C31HeldBuilder builder) {
			this.h = builder.getH();
		}
		
		@Override
		@RosettaAttribute("h")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("h")
		public String getH() {
			return h;
		}
		
		@Override
		public C31Held build() {
			return this;
		}
		
		@Override
		public C31Held.C31HeldBuilder toBuilder() {
			C31Held.C31HeldBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C31Held.C31HeldBuilder builder) {
			ofNullable(getH()).ifPresent(builder::setH);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C31Held _that = getType().cast(o);
		
			if (!Objects.equals(h, _that.getH())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (h != null ? h.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C31Held {" +
				"h=" + this.h +
			'}';
		}
	}

	/*********************** Builder Implementation of C31Held  ***********************/
	class C31HeldBuilderImpl implements C31Held.C31HeldBuilder {
	
		protected String h;
		
		@Override
		@RosettaAttribute("h")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("h")
		public String getH() {
			return h;
		}
		
		@RosettaAttribute("h")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("h")
		@Override
		public C31Held.C31HeldBuilder setH(String _h) {
			this.h = _h == null ? null : _h;
			return this;
		}
		
		@Override
		public C31Held build() {
			return new C31Held.C31HeldImpl(this);
		}
		
		@Override
		public C31Held.C31HeldBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C31Held.C31HeldBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getH()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C31Held.C31HeldBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C31Held.C31HeldBuilder o = (C31Held.C31HeldBuilder) other;
			
			
			merger.mergeBasic(getH(), o.getH(), this::setH);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C31Held _that = getType().cast(o);
		
			if (!Objects.equals(h, _that.getH())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (h != null ? h.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C31HeldBuilder {" +
				"h=" + this.h +
			'}';
		}
	}
}
