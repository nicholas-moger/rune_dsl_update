package chaos.s16.a2qual.h;

import chaos.s16.a2qual.h.meta.C16HeldMeta;
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
 * Self-contained helper - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C16Held", builder=C16Held.C16HeldBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C16Held", model="chaos", builder=C16Held.C16HeldBuilderImpl.class, version="1.0.0")
public interface C16Held extends RosettaModelObject {

	C16HeldMeta metaData = new C16HeldMeta();

	/*********************** Getter Methods  ***********************/
	String getH();

	/*********************** Build Methods  ***********************/
	C16Held build();
	
	C16Held.C16HeldBuilder toBuilder();
	
	static C16Held.C16HeldBuilder builder() {
		return new C16Held.C16HeldBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C16Held> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C16Held> getType() {
		return C16Held.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("h"), String.class, getH(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C16HeldBuilder extends C16Held, RosettaModelObjectBuilder {
		C16Held.C16HeldBuilder setH(String h);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("h"), String.class, getH(), this);
		}
		

		C16Held.C16HeldBuilder prune();
	}

	/*********************** Immutable Implementation of C16Held  ***********************/
	class C16HeldImpl implements C16Held {
		private final String h;
		
		protected C16HeldImpl(C16Held.C16HeldBuilder builder) {
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
		public C16Held build() {
			return this;
		}
		
		@Override
		public C16Held.C16HeldBuilder toBuilder() {
			C16Held.C16HeldBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C16Held.C16HeldBuilder builder) {
			ofNullable(getH()).ifPresent(builder::setH);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16Held _that = getType().cast(o);
		
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
			return "C16Held {" +
				"h=" + this.h +
			'}';
		}
	}

	/*********************** Builder Implementation of C16Held  ***********************/
	class C16HeldBuilderImpl implements C16Held.C16HeldBuilder {
	
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
		public C16Held.C16HeldBuilder setH(String _h) {
			this.h = _h == null ? null : _h;
			return this;
		}
		
		@Override
		public C16Held build() {
			return new C16Held.C16HeldImpl(this);
		}
		
		@Override
		public C16Held.C16HeldBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16Held.C16HeldBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getH()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16Held.C16HeldBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C16Held.C16HeldBuilder o = (C16Held.C16HeldBuilder) other;
			
			
			merger.mergeBasic(getH(), o.getH(), this::setH);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16Held _that = getType().cast(o);
		
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
			return "C16HeldBuilder {" +
				"h=" + this.h +
			'}';
		}
	}
}
