package chaos.s17.a5mixed;

import chaos.s17.a5mixed.meta.C17HeldMeta;
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
@RosettaDataType(value="C17Held", builder=C17Held.C17HeldBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C17Held", model="chaos", builder=C17Held.C17HeldBuilderImpl.class, version="1.0.0")
public interface C17Held extends RosettaModelObject {

	C17HeldMeta metaData = new C17HeldMeta();

	/*********************** Getter Methods  ***********************/
	String getH();

	/*********************** Build Methods  ***********************/
	C17Held build();
	
	C17Held.C17HeldBuilder toBuilder();
	
	static C17Held.C17HeldBuilder builder() {
		return new C17Held.C17HeldBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C17Held> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C17Held> getType() {
		return C17Held.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("h"), String.class, getH(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C17HeldBuilder extends C17Held, RosettaModelObjectBuilder {
		C17Held.C17HeldBuilder setH(String h);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("h"), String.class, getH(), this);
		}
		

		C17Held.C17HeldBuilder prune();
	}

	/*********************** Immutable Implementation of C17Held  ***********************/
	class C17HeldImpl implements C17Held {
		private final String h;
		
		protected C17HeldImpl(C17Held.C17HeldBuilder builder) {
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
		public C17Held build() {
			return this;
		}
		
		@Override
		public C17Held.C17HeldBuilder toBuilder() {
			C17Held.C17HeldBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C17Held.C17HeldBuilder builder) {
			ofNullable(getH()).ifPresent(builder::setH);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C17Held _that = getType().cast(o);
		
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
			return "C17Held {" +
				"h=" + this.h +
			'}';
		}
	}

	/*********************** Builder Implementation of C17Held  ***********************/
	class C17HeldBuilderImpl implements C17Held.C17HeldBuilder {
	
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
		public C17Held.C17HeldBuilder setH(String _h) {
			this.h = _h == null ? null : _h;
			return this;
		}
		
		@Override
		public C17Held build() {
			return new C17Held.C17HeldImpl(this);
		}
		
		@Override
		public C17Held.C17HeldBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C17Held.C17HeldBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getH()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C17Held.C17HeldBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C17Held.C17HeldBuilder o = (C17Held.C17HeldBuilder) other;
			
			
			merger.mergeBasic(getH(), o.getH(), this::setH);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C17Held _that = getType().cast(o);
		
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
			return "C17HeldBuilder {" +
				"h=" + this.h +
			'}';
		}
	}
}
