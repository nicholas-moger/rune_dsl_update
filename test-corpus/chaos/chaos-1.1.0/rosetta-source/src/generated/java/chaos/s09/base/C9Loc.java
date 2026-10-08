package chaos.s09.base;

import chaos.s09.base.meta.C9LocMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.annotations.RuneScopedAttributeKey;
import com.rosetta.model.lib.annotations.RuneScopedAttributeReference;
import com.rosetta.model.lib.meta.Key;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.ReferenceWithMetaString;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * location + address pair.
 * @version 1.0.0
 */
@RosettaDataType(value="C9Loc", builder=C9Loc.C9LocBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C9Loc", model="chaos", builder=C9Loc.C9LocBuilderImpl.class, version="1.0.0")
public interface C9Loc extends RosettaModelObject {

	C9LocMeta metaData = new C9LocMeta();

	/*********************** Getter Methods  ***********************/
	FieldWithMetaString getSpot();
	ReferenceWithMetaString getPtr();

	/*********************** Build Methods  ***********************/
	C9Loc build();
	
	C9Loc.C9LocBuilder toBuilder();
	
	static C9Loc.C9LocBuilder builder() {
		return new C9Loc.C9LocBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C9Loc> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C9Loc> getType() {
		return C9Loc.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("spot"), processor, FieldWithMetaString.class, getSpot());
		processRosetta(path.newSubPath("ptr"), processor, ReferenceWithMetaString.class, getPtr());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C9LocBuilder extends C9Loc, RosettaModelObjectBuilder {
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateSpot();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getSpot();
		ReferenceWithMetaString.ReferenceWithMetaStringBuilder getOrCreatePtr();
		@Override
		ReferenceWithMetaString.ReferenceWithMetaStringBuilder getPtr();
		C9Loc.C9LocBuilder setSpot(FieldWithMetaString spot);
		C9Loc.C9LocBuilder setSpotValue(String spot);
		C9Loc.C9LocBuilder setPtr(ReferenceWithMetaString ptr);
		C9Loc.C9LocBuilder setPtrValue(String ptr);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("spot"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getSpot());
			processRosetta(path.newSubPath("ptr"), processor, ReferenceWithMetaString.ReferenceWithMetaStringBuilder.class, getPtr());
		}
		

		C9Loc.C9LocBuilder prune();
	}

	/*********************** Immutable Implementation of C9Loc  ***********************/
	class C9LocImpl implements C9Loc {
		private final FieldWithMetaString spot;
		private final ReferenceWithMetaString ptr;
		
		protected C9LocImpl(C9Loc.C9LocBuilder builder) {
			this.spot = ofNullable(builder.getSpot()).map(f->f.build()).orElse(null);
			this.ptr = ofNullable(builder.getPtr()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("spot")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("spot")
		@RuneScopedAttributeKey
		public FieldWithMetaString getSpot() {
			return spot;
		}
		
		@Override
		@RosettaAttribute("ptr")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("ptr")
		@RuneScopedAttributeReference
		public ReferenceWithMetaString getPtr() {
			return ptr;
		}
		
		@Override
		public C9Loc build() {
			return this;
		}
		
		@Override
		public C9Loc.C9LocBuilder toBuilder() {
			C9Loc.C9LocBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C9Loc.C9LocBuilder builder) {
			ofNullable(getSpot()).ifPresent(builder::setSpot);
			ofNullable(getPtr()).ifPresent(builder::setPtr);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C9Loc _that = getType().cast(o);
		
			if (!Objects.equals(spot, _that.getSpot())) return false;
			if (!Objects.equals(ptr, _that.getPtr())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (spot != null ? spot.hashCode() : 0);
			_result = 31 * _result + (ptr != null ? ptr.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C9Loc {" +
				"spot=" + this.spot + ", " +
				"ptr=" + this.ptr +
			'}';
		}
	}

	/*********************** Builder Implementation of C9Loc  ***********************/
	class C9LocBuilderImpl implements C9Loc.C9LocBuilder {
	
		protected FieldWithMetaString.FieldWithMetaStringBuilder spot;
		protected ReferenceWithMetaString.ReferenceWithMetaStringBuilder ptr;
		
		@Override
		@RosettaAttribute("spot")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("spot")
		@RuneScopedAttributeKey
		public FieldWithMetaString.FieldWithMetaStringBuilder getSpot() {
			return spot;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateSpot() {
			FieldWithMetaString.FieldWithMetaStringBuilder result;
			if (spot!=null) {
				result = spot;
			}
			else {
				result = spot = FieldWithMetaString.builder();
				result.getOrCreateMeta().toBuilder().addKey(Key.builder().setScope("DOCUMENT"));
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("ptr")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("ptr")
		@RuneScopedAttributeReference
		public ReferenceWithMetaString.ReferenceWithMetaStringBuilder getPtr() {
			return ptr;
		}
		
		@Override
		public ReferenceWithMetaString.ReferenceWithMetaStringBuilder getOrCreatePtr() {
			ReferenceWithMetaString.ReferenceWithMetaStringBuilder result;
			if (ptr!=null) {
				result = ptr;
			}
			else {
				result = ptr = ReferenceWithMetaString.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("spot")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("spot")
		@RuneScopedAttributeKey
		@Override
		public C9Loc.C9LocBuilder setSpot(FieldWithMetaString _spot) {
			this.spot = _spot == null ? null : _spot.toBuilder();
			return this;
		}
		
		@Override
		public C9Loc.C9LocBuilder setSpotValue(String _spot) {
			this.getOrCreateSpot().setValue(_spot);
			return this;
		}
		
		@RosettaAttribute("ptr")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("ptr")
		@RuneScopedAttributeReference
		@Override
		public C9Loc.C9LocBuilder setPtr(ReferenceWithMetaString _ptr) {
			this.ptr = _ptr == null ? null : _ptr.toBuilder();
			return this;
		}
		
		@Override
		public C9Loc.C9LocBuilder setPtrValue(String _ptr) {
			this.getOrCreatePtr().setValue(_ptr);
			return this;
		}
		
		@Override
		public C9Loc build() {
			return new C9Loc.C9LocImpl(this);
		}
		
		@Override
		public C9Loc.C9LocBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C9Loc.C9LocBuilder prune() {
			if (spot!=null && !spot.prune().hasData()) spot = null;
			if (ptr!=null && !ptr.prune().hasData()) ptr = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getSpot()!=null) return true;
			if (getPtr()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C9Loc.C9LocBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C9Loc.C9LocBuilder o = (C9Loc.C9LocBuilder) other;
			
			merger.mergeRosetta(getSpot(), o.getSpot(), this::setSpot);
			merger.mergeRosetta(getPtr(), o.getPtr(), this::setPtr);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C9Loc _that = getType().cast(o);
		
			if (!Objects.equals(spot, _that.getSpot())) return false;
			if (!Objects.equals(ptr, _that.getPtr())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (spot != null ? spot.hashCode() : 0);
			_result = 31 * _result + (ptr != null ? ptr.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C9LocBuilder {" +
				"spot=" + this.spot + ", " +
				"ptr=" + this.ptr +
			'}';
		}
	}
}
